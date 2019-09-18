cd /Users/618715/program/github-git/biu
cd browser
npm run build:prod
cd ../server
mvn package -Dmaven.test.skip=true
scp /Users/618715/program/github-git/biu/server/target/biu-1.0.jar appdeploy@10.203.14.137:/app/war/biu
