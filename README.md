Connekt Platform
========================

v2 Communications Platform

Getting Started
---------------------

#### Clone
1. Checkout the repo `git clone <url>`
2. Let's add a custom remote which will keep github and gitcorp in sync ( let's call it 'all')

	```bash
	git remote add all git.corp.flipkart.com:/git/mp/fk-connekt
	git remote set-url --add --push all git.corp.flipkart.com:/git/mp/fk-connekt
	git remote set-url --add --push all git@github.com:Flipkart/fk-connekt.git
	```
3. Make your changes
4. Push your changes

	```bash
	git add <files>
	...
	git commit -m "What changes does this commit makes?"
	git push
	```

#### Promoting

	```bash
	./scripts/fk-git-promote -e <enviroment> -b <branch> -p <package>
	```

Contributing
-------------------------

Create a branch for all feature developement's and create a pull request when you feel that the code is production ready and ready for review and merge.



