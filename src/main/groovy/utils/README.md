# Utilities

## Child count provider

Load url

    java -Dpath="/content" -jar oak-run*.jar console /path/to/segmentstore ":load https://raw.githubusercontent.com/chetanmeh/oak-console-scripts/master/src/main/groovy/utils/countChildren.groovy"
    
Specify path via system property `path`

## Last rev recovery

Known to work with: 1.8.12

Command line for dry run (example):

    java -Dpath="/content/site/en/home" -DrecoverClusterId=3 -jar oak-run*.jar console mongodb://localhost:27017/dbname ":load lastRevRecovery.groovy"
    
Specify the path to run recovery on via system property `path` and the clusterId
to run recovery for via system property `recoverClusterId`. The script will run
multiple recovery cycles, starting at the specified path and traversing up to
the root node.

When the dry run returned expected results, run the same command with the
`--read-write` flag to actually perform the recovery (example):

    java -Dpath="/content/site/en/home" -DrecoverClusterId=3 -jar oak-run*.jar console --read-write mongodb://localhost:27017/dbname ":load lastRevRecovery.groovy"
