#!/usr/bin/env python3
"""Official v160.5 server integration. Requires VENDOR; test classes are never shipped in the mod."""
import argparse, os, pathlib, queue, re, shutil, subprocess, tempfile, threading, time, zipfile
ROOT=pathlib.Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser();p.add_argument('--mode',choices=['integration','migration'],default='integration');p.add_argument('--baseline',type=pathlib.Path);p.add_argument('--jar',type=pathlib.Path);args=p.parse_args()
VENDOR=pathlib.Path(os.environ.get('VENDOR',pathlib.Path.home()/'.cache/vendor'))
JDK=pathlib.Path(os.environ.get('JAVA_HOME',next(iter(VENDOR.glob('jdk-17*')),VENDOR/'missing-jdk')))
JAVA=JDK/'bin/java';JAVAC=JDK/'bin/javac'
JAR=args.jar or next((f for f in [ROOT/'dist/Singularity-v8.4-beta.jar',ROOT/'Singularity-v8.4-beta.jar',ROOT.parent/'Singularity-v8.4-beta.jar'] if f.exists()),ROOT/'dist/Singularity-v8.4-beta.jar')
REPORT=ROOT/'docs/v84';REPORT.mkdir(parents=True,exist_ok=True)
TMP=pathlib.Path(tempfile.mkdtemp(prefix='sing-v84-test-'))
ANSI=re.compile(r'\x1b\[[0-9;]*m')
class Server:
    def __init__(self,jar,name):
        self.cwd=TMP/name;(self.cwd/'config/mods').mkdir(parents=True)
        shutil.copy2(jar,self.cwd/'config/mods/Singularity.jar');self.q=queue.Queue();self.log=(REPORT/(name+'.log')).open('w')
        env=os.environ.copy();env['HOME']=str(self.cwd)
        self.p=subprocess.Popen([str(JAVA),'-Xmx850m','-Dv84.report='+str(REPORT),'-Dv84.oldsave='+str(REPORT/'v83-migration.msav'),'-jar',str(VENDOR/'server-release.jar')],cwd=self.cwd,env=env,stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,bufsize=1)
        def reader():
            for line in self.p.stdout:
                line=ANSI.sub('',line).rstrip();self.log.write(line+'\n');self.log.flush();self.q.put(line)
                if any(k in line for k in ['V84-','Error','Exception','Assertion','Server loaded','[blackhole/v8.4]']):print(line,flush=True)
            self.q.put('__EXIT__')
        threading.Thread(target=reader,daemon=True).start();self.wait('Server loaded.',120)
    def wait(self,token,timeout=240):
        deadline=time.monotonic()+timeout
        while time.monotonic()<deadline:
            try:line=self.q.get(timeout=max(.1,deadline-time.monotonic()))
            except queue.Empty:break
            if 'V84-FAIL:' in line or line=='__EXIT__':raise RuntimeError(line)
            if token in line:return
        raise TimeoutError(token)
    def command(self,command,token,timeout=240):
        self.p.stdin.write(command+'\n');self.p.stdin.flush();self.wait(token,timeout)
    def close(self):
        if self.p.poll() is None:
            try:self.p.stdin.write('exit\n');self.p.stdin.flush();self.p.wait(timeout=20)
            except Exception:self.p.terminate();self.p.wait(timeout=10)
        self.log.close()
def instrument():
    classes=TMP/'classes';classes.mkdir()
    subprocess.run([str(JAVAC),'--release','17','-cp',str(JAR)+os.pathsep+str(VENDOR/'server-release.jar'),'-d',str(classes),str(ROOT/'tests/V84Integration.java')],check=True)
    jar=TMP/'instrumented.jar';shutil.copy2(JAR,jar)
    with zipfile.ZipFile(jar,'a',compression=zipfile.ZIP_DEFLATED) as z:
        for f in classes.rglob('*.class'):z.write(f,f.relative_to(classes).as_posix())
    return jar
s=None
try:
    if args.mode=='migration':
        if not args.baseline:raise ValueError('--baseline must name the original v8.3 jar or source ZIP')
        old=TMP/'baseline.jar'
        if args.baseline.name.endswith('.jar'):shutil.copy2(args.baseline,old)
        else:
            with zipfile.ZipFile(args.baseline) as z:
                names=[n for n in z.namelist() if n.endswith('Singularity-v8.3-beta.jar')]
                if len(names)!=1:raise ValueError('baseline archive must have exactly one original v8.3 jar')
                old.write_bytes(z.read(names[0]))
        s=Server(old,'old-v83-fixture')
        fixture='''var p=Vars.content.planet("blackhole-aurelia");var sec=p.sectors.get(65);Vars.logic.reset();Vars.world.loadSector(sec);Vars.state.rules.sector=sec;Vars.logic.play();var core=Team.sharded.core();var x=core.tile.x+35,y=core.tile.y+35;for(var dx=-20;dx<=20;dx++)for(var dy=-20;dy<=20;dy++){var q=Vars.world.tile(x+dx,y+dy);q.setBlock(Blocks.air);q.setFloor(Vars.content.block("blackhole-aurora-plate"));q.setOverlay(Blocks.air);}Vars.world.tile(x,y).setBlock(Vars.content.block("blackhole-ward-dome"),Team.sharded,0);Vars.world.tile(x,y).build.shield=3456;Vars.world.tile(x+10,y).setBlock(Vars.content.block("blackhole-astro-commander"),Team.sharded,0);core.items.set(Vars.content.item("blackhole-lumenite"),777);Vars.state.wave=100;Vars.state.rules.waves=true;sec.info.waves=true;sec.info.wasCaptured=false;Packages.mindustry.io.SaveIO.save(new Packages.arc.files.Fi(java.lang.System.getProperty("v84.oldsave")));new Packages.arc.files.Fi(java.lang.System.getProperty("v84.oldsave")).sibling("v83-sector-info.json").writeString(Packages.mindustry.io.JsonIO.write(sec.info));"V84-FIXTURE-SAVED"'''
        s.command('js '+fixture,'V84-FIXTURE-SAVED');s.close();s=None
    s=Server(instrument(),'integration' if args.mode=='integration' else 'migration')
    method='runSafe' if args.mode=='integration' else 'oldSafe'
    command='js Vars.content.block("blackhole-ward-dome").getClass().getClassLoader().loadClass("blackhole.tests.V84Integration").getMethod("'+method+'").invoke(null)'
    s.command(command,'V84-ALL-PASSED' if args.mode=='integration' else 'V84-OLD-PASSED',420)
    print('PASS: '+args.mode,flush=True)
finally:
    if s:s.close()
    shutil.rmtree(TMP,ignore_errors=True)
