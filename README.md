
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

## License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")
