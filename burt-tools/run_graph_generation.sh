export CUR_DIR=`pwd`


cd $CUR_DIR

cd ../burt-nlparser && mvn clean install -DskipTests
#cd ../crashscope && mvn clean install -DskipTests
cd ../burt-quality-checker && mvn clean install -DskipTests
cd $CUR_DIR

mvn package -DskipTests
java -cp target/burt-tools-0.0.1.jar MainJSONGraphGenerator