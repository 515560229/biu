cd /Users/618715/program/github-git/biu
cd browser
npm run build:prod
cd ../
gradle build -x test
scp /Users/618715/program/github-git/biu/server/build/libs/server-1.0.jar appdeploy@10.203.14.137:/app/war/biu