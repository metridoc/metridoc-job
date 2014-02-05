Job Template
------------

This is a basic job template that can be used to create metridoc jobs.  To install, simply 
[download](https://github.com/metridoc/metridoc-template-job/archive/master.zip) the project and unzip it.  Name the
directory where it is stored with the following convention `metridoc-job-<project name>`.  

To run the job, navigate to the directory and run `mdoc .`.  Assuming you named the job `metridoc-job-foo`, you can 
install the job by doing `mdoc install .`, and run `mdoc foo`.  If you install the job, it will be located at 
`~/.metridoc/jobs`.  Any subsequant installs will overwrite the job.  To see what jobs you have installed, simply run
`mdoc list-jobs`.

####Project Structure

```
├── README.md
├── build.gradle
├── gradle
│   └── wrapper
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── gradle.properties
├── gradlew
├── gradlew.bat
└── src
    ├── main
    │   └── groovy
    │       └── metridoc.groovy
    └── test
        └── groovy
            └── SomeSpec.groovy
```

Every job requires an entry point.  `metridoc.groovy` is the project entry point.  Based on the name of your project,
you can change this.  If the project name is `metridoc-job-foo`, you can name this file `foo.groovy`.  Running the project
would work exactly the same, `mdoc .` or `mdoc foo` if it is installed.


####Accessing Your Job From Github


