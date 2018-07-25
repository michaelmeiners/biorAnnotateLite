# set -x
# CLASSPATH=`ls $BIOR_LITE_HOME/lib/*.jar | tr '\n' ':'`
# CLASSPATH=$BIOR_LITE_HOME/lib/samtools-1.5.6.jar
CLASSPATH=/usr/local/biotools/bior_scripts/4.3.1/bior_pipeline-4.3.1/lib/htsjdk-1.143.jar

export GROOVY_HOME=/data5/bsi/BIOR/groovy-2.4.3
export PATH=$GROOVY_HOME/bin:$PATH

DIR_THIS_SCRIPT_IS_IN=$(dirname "$0")
groovy -cp $CLASSPATH $DIR_THIS_SCRIPT_IS_IN/merge.groovy $* 

