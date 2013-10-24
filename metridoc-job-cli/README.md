metridoc-job-cli
================
[![Build Status](https://drone.io/github.com/metridoc/metridoc-job-cli/status.png)](https://drone.io/github.com/metridoc/metridoc-job-cli/latest)

For complete documentation see the metridoc [wiki](https://github.com/metridoc/metridoc-job/wiki)

#### Installation

in a bash environment, simply run 
`curl -s https://raw.github.com/metridoc/metridoc-job-cli/master/src/etc/install-mdoc.sh | sh`, otherwise one can download
the distribution from bintray at https://bintray.com/upennlib/metridoc-distributions and set path variables appropriatelly.

#### Quick Start
----------------

NOTE: visit the [wiki](https://github.com/metridoc/metridoc-wiki/wiki) for a more detailed discussion about the tool

run `mdoc` to get some basic usage information.  `mdoc` can install a job or simply run a groovy script.  

##### Scripting

run `mdoc <some script>` to run a groovy script.  Not only will you have access to the groovy libraries, but also various
other integration libraries.  This in mind, the simplest metridoc script is nothing more than a groovy script that prints
hello world.

```groovy
println "hello world"
```

assuming you stored this into a file called `Hello.groovy`, you can run `mdoc Hello.groovy`.  For more complex examples
please see the [recipes](https://github.com/metridoc/metridoc-job-cli/tree/master/src/recipes) section of the code.

#### Jobs

