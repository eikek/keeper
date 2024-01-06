# keeper - keep track of your bikes and changes to them

Keeper aims to help you keeping track of changes to your bikes.
Imagine, after a few decisions, you end up with more than one bicycle,
perhaps three wheelsets and multiple chains and tires. How to know how
far each tire or wheel has travelled over the years when you change
wheels between bikes etc? Keeper can maintain this knowledge. The idea
is that whenever you modify your bikes, you record the mileage of
every bike at that time. Keeper then "keeps track" of every
modification and can calculate the mileage of every component at any
point in time. You can go back in time and look how your bikes were
configured in the past.

You can type in the distance of your bikes at a maintenance or get it
from Strava or [Fit4S](https://github.com/eikek/fit4s).

## Building

[sbt](https://scala-sbt.org) is used to build the app. Use the
`make-cli.sh` script to create a zip package in one go.

## Usage

At the beginning, there is a tedious task of adding every component
you own to your "inventory". The inventory is a virtual shelf of bike
components (including those currently mounted on a bike). A bike
component is associated to a product, which must be added first. Be
considerate with the "added date" of a component. This date indicates
when the component arrived at your inventory.

After this process, you can create bikes by configuring them using
components from your inventory. The date you select for your new bike
day specifies which components are available for configuration. Bike
creation only selects components that have been added _before_. Then
you can do maintenances. A maintenance consists of possibly many
"maintenance events" like changing tires, swapping a chain etc.

A maintenance is added to a "maintenance log" that shows every
modification and allows for some querying. You can select a date up to
which the maintenance log is rendered, allowing to go back in time.

## Tech Stack

The core library only implements the FIT codec using the scodec
library. The CLI application uses more libraries:

- [Scala 3](https://scala-lang.org) all the way, [Scala.js](https://www.scala-js.org/) for the web frontend
- based on [cats-effect](https://github.com/typelevel/cats-effect) and [fs2](https://github.com/typelevel/fs2)
- [skunk](https://github.com/typelevel/skunk) for DB access
- [http4s](https://github.com/http4s/http4s) for the http api
- [decline](https://github.com/bkirwi/decline) for parsing cli options
- [ciris](https://github.com/vlovgr/ciris) for configuration
- [borer](https://github.com/sirthias/borer) for JSON encoding/decoding
- [scribe](https://github.com/outr/scribe) for logging
- [calico](https://github.com/armanbilge/calico) for Scala.js based web application
- [tailwind](https://tailwindcss.com/) for styling (css)


## Development

This is a Scala 3 project. For development, install `npm` and `sbt` or
use the provided nix setup. You can drop into a shell with `nix
develop`.

Run `sbt compile` to compile the whole project.

For developing the webclient with ScalaJS, use two terminals. The
first runs sbt, where first the http server is started and then the
watch command runs to build the javascript using ScalaJS. The second
terminal runs `npm` that will react on newly build js files and runs
the frontend build.

Terminal 1:
```
> sbt
…
sbt:keeper-root> cli/reStart server start
…
cli 2023.07.25 19:55:06:095 io-compute-11 INFO org.http4s.ember.server.EmberServerBuilderCompanionPlatform
cli     Ember-Server service bound to address: 127.0.0.1:8182
cli Started webview server at 127.0.0.1:8182
sbt:keeper-root> ~webviewJS/fastLinkJS
…
[info] 1. Monitoring source files for root/webviewJS/fastLinkJS...
[info]    Press <enter> to interrupt or '?' for more options.
```

Terminal 2:
```
> cd modules/webview
> npm run dev

  VITE v4.3.9  ready in 23342 ms

  ➜  Local:   http://localhost:5173/
  ➜  Network: use --host to expose
  ➜  press h to show help
```

If a scala file in `modules/webview/{js,shared}` is changed, the first
terminal compiles a new javascript output and the second terminal
builds a new version of the frontend.

The http api is at `localhost:8182` and the frontend at
`localhost:5173`.

This project started out from the tutorial for [ScalaJS and
Vite](https://www.scala-js.org/doc/tutorial/scalajs-vite.html).
