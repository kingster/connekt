Connekt Platform
========================

Communications Platform

Tech Stack 
---------------------
* [Scala] - Scala 2.11
* [Akka] - Akka 2.4.4
* [ChronosQ] - ChronosQ 1.1

[Scala]:http://www.scala-lang.org
[Akka]:http://doc.akka.io/docs/akka/2.4.4/scala.html
[ChronosQ]:https://github.com/flipkart-incubator/chronosq

API Docs
---------------------

- You can find sample requests on our API [sample doc's](https://docs.google.com/document/d/1SgIZHrXTTSmuvL-fyr7hLV7Dzwg_npgu4OHtUanztYk/edit#).
- Swagger-UI Available at [connekt-swagger](http://fk-connekt-sandbox-0001.nm.flipkart.com:9991/), just browse to [api-definition](https://github.com/Flipkart/connekt-toolbox/blob/HEAD/schema/swagger-api.yaml) and copy paste the raw-url and hit explore!

Getting Started
---------------------

#### Fork & Clone ###
1. Click on fork and clone the repo!
2. Checkout the fork repo `git clone <url>`
3. Let's create a branch to track these changes.
	
	```bash
	git checkout -b <branch-name>
	```

4. Push your changes

	```bash
	git add <files>
	...
	git commit -m "CNKT-<JIRA-NO> What changes does this commit make ?"
	git push
	```

5. Raise a [pull request](https://help.github.com/articles/creating-a-pull-request/) so that others may review and merge it.

#### Running Locally
To run the project locally ``sbt compile`` followed by ``sbt run `` and follow the instructions. For running receptors you will need to run ``sbt "run receptors"``.

#### Promoting
In order to promote your changes and build the package, refer to the jenkins job at [Connekt Promote](https://jenkins-master-cp.nm.flipkart.com/view/Connekt/job/promote_connekt/) to trigger build

#### Deployment
Refer to the [deployment guide](https://github.com/Flipkart/connekt-toolbox/blob/HEAD/resources/deployer/README.md) for more details.

### Toolbox
Utitilies & Scripts at [toolbox-repo](https://github.com/Flipkart/connekt-toolbox) 

Contributing
-------------------------

1. Create a branch for all feature developments.
2. Ensure, your changes have corresponding UTs, so that functional correctness can be readily verified.
3. Create a pull request when you feel that the code is production ready and ready for review and merge. ![Warning](http://icons.iconarchive.com/icons/paomedia/small-n-flat/16/sign-warning-icon.png) Please ensure that you do both sanity testing and perf testing before you raise any request! _Even a single line change may lead to deterioration of the entire flow_.





