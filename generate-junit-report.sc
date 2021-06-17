import scala.collection.mutable.ArrayBuffer

case class Trace(
    declaringClass: String,
    methodName: String,
    fileName: String,
    lineNumber: Int
) {
  override def toString: String =
    s"${declaringClass}.${methodName}(${fileName}:${lineNumber})"
}

case class Failure(
    name: String,
    message: String,
    trace: Seq[Trace]
)

case class Test(
    fullyQualifiedName: String,
    selector: String,
    duration: Double,
    failure: Option[Failure]
)

@main
def main(id: String, name: String, into: os.Path, reports: os.Path*): Unit = {
  val tests: Seq[Test] = reports.map(x => ujson.read(x.toNIO)).flatMap { json =>
    json(1).value.asInstanceOf[ArrayBuffer[ujson.Obj]].map { test =>
      Test(
        fullyQualifiedName = test("fullyQualifiedName").str,
        selector = test("selector").str,
        duration = test("duration").num / 1000.0,
        failure = test("status").str match {
          case "Failure" => Some(Failure(
              name = test("exceptionName")(0).str,
              message = test("exceptionMsg")(0).str,
              trace = test("exceptionTrace")(0).arr.map { st =>
                val declaringClass = st("declaringClass").str
                val methodName     = st("methodName").str
                val fileName       = st("fileName")(0).str
                val lineNumber     = st("lineNumber").num.toInt
                Trace(declaringClass, methodName, fileName, lineNumber)
              }.toList
            ))
          case _ => None
        }
      )
    }
  }

  val suites = tests.groupBy(_.fullyQualifiedName).map { case (suit, tests) =>
    val testcases = tests.map { test =>
      <testcase id={test.selector} classname={test.fullyQualifiedName} name={
        test.selector.substring(test.fullyQualifiedName.length + 1)
      } time={test.duration.toString}>
        {
        test.failure.map { failure =>
          <failure message={failure.message} type="ERROR">
ERROR: {failure.message}
Category: {failure.name}
File: {failure.trace(1).fileName}
Line: {failure.trace(1).lineNumber}
          </failure>
        }.orNull
      }
        {
        test.failure.map { failure =>
          <system-err>{
            failure.trace.mkString(s"${failure.name}: ${failure.message}", "\n    at ", "")
          }</system-err>
        }.orNull
      }
        </testcase>
    }

    <testsuite id={suit} name={suit} tests={tests.length.toString} failures={
      tests.count(_.failure.isDefined).toString
    } time={tests.map(_.duration).sum.toString}>
      {testcases}
    </testsuite>
  }

  val node = <testsuites id={id} name={name} tests={tests.length.toString} failures={
    tests.count(_.failure.isDefined).toString
  } time={tests.map(_.duration).sum.toString}>{suites}</testsuites>
  scala.xml.XML.save(filename = into.toString(), node = node, xmlDecl = true)
}
