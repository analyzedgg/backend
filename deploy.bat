SET /P username=Please enter username:
call sbt assembly
cd target\scala-2.11
scp api-assembly-0.0.1.jar %username%@analyzed.gg:work/docker/deploy/api-0.0.1.jar
cd ..\..\
ssh %username%@analyzed.gg 'work/docker/deploy/deploy.sh'