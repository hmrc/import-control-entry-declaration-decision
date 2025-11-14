
# Import Control Entry Declaration Decision

The Import Control Entry Declaration Decision responsibilities:
- receive JSON decisions from C&IT
- transform the decision to XML and send to Outcome microservice

## Development Setup
- Run locally: `sbt run` which runs on port `9816` by default
- Run with test end points: `sbt 'run -Dplay.http.router=testOnlyDoNotUseInAppConf.Routes'`

## Tests
- Run Unit Tests: `sbt test`

## API

|Path | Supported Methods | Type | Description |
| --------------------------------------------------------| -----| ---------| ---------|
|```/import-control/entry-summary-declaration-response``` | POST | Internal | Endpoint for C&IT to return a decision for an ENS submission. |

### All tests and checks

> `sbt runAllChecks`

This is a sbt command alias specific to this project. It will run

- clean
- compile
- unit tests
- and produce a coverage report.

You can view the coverage report in the browser by pasting the generated url.

#### Installing sbt plugin to check for library updates.
To check for dependency updates locally you will need to create this file locally ~/.sbt/1.0/plugins/sbt-updates.sbt
and paste - addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.6.3") - into the file.
Then run:

> `sbt dependencyUpdates `

To view library update suggestions - this does not cover sbt plugins.
It is not advised to install the plugin for the project.


## License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")
