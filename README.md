# Generate JUnit XML reports from Mill's test output.

Many CI/CD servers already support reading JUnit XML reports and integrate well with code-review and merge-requests.
However [Mill](https://com-lihaoyi.github.io/mill/mill/Intro_to_Mill.html) is still more of a Scala niche tool.

This repo contains a tiny Scala utility that can transform [Mill test](https://com-lihaoyi.github.io/mill/mill/Configuring_Mill.html#_adding_a_test_suite)
json reports into [JUnit XML reports](https://www.ibm.com/docs/en/developer-for-zos/14.1.0?topic=formats-junit-xml-format). 


### Usage

```shell
ammonite generate-junit-report.sc --help

ammonite generate-junit-report.sc TEST_ID TEST_NAME TARGET_JUNIT_XML_FILE SOURCE_MILL_JSON_FILE...
```

`TEST_ID` and `TEST_NAME` have no special meaning and are for you to decide. You can provide many json files as input and they will be aggreated into a single target xml file.

### Example

Suppose you have a `build.sc` file like:

```scala
// build.sc
import mill._, scalalib._

object foo extends ScalaModule {
  def scalaVersion = "2.13.1"

  object test extends Tests {
    def ivyDeps = Agg(ivy"com.lihaoyi::utest:0.7.1")
    def testFramework = "utest.runner.Framework"
  }
}
```

After running `mill foo.test` you will find mill's json report at 
`out/foo/test/test/dest/out.json`. 

To convert it into JUnit XML format, execute the following ammonite script:

```shell
> ammonite generate-junit-report.sc "foo.test" "Foo tests" "foo-junit.xml" "out/foo/test/test/dest/out.json"
```

that will generate `foo-junit.xml`.
