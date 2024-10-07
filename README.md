# Release-notes-generator
A helper project to generate release notes for our web applications.

I got the idea for this project from this article: [Creating a Command Line Tool with JBang and PicoCLI to Generate Release Notes](https://foojay.io/today/creating-a-command-line-tool-with-jbang-and-picocli-to-generate-release-notes/).

The original code can be found here: https://gist.github.com/rokon12/fd039cdcfa98920ea9e881bf18e33b0b


## Requirements

### TL;DR
1. Java 17 or higher version
2. `gh` command line tool
3. At the 1st run of the application you can find a validation token for GitHub API access.
You need to copy that code and authenticate with GitHub using that token.

### Longer requirements description

This code fetches your GitHub API token securely. 
It first checks if a cached token exists. 
If not, it uses the `gh` command-line tool to get your authentication status. 
It launches the `gh` login process if you're not logged in. 
Once logged in, it extracts your API token from the `gh` output and caches it for future use.
If there are any errors during this process, it throws an exception.

***Important Note:*** This Application relies on the GitHub CLI (`gh`). 
If you haven't already installed it, you can find [instructions](https://github.com/cli/cli?tab=readme-ov-file#installation) for your operating system.


## How to use it

It is a command line script. It can be run either from a command line or from an IDE execution environment.

This application has a list of mandatory parameters:

- `-u` or `-user` GitHub username
- `-r` or `--repo` GitHub repository name
- `-s` or `--since` Since commit hash
- `-ut` or `--until` Until commit hash

Optional parameters:

- `-f` or `--file` Output file for release notes
- `-v` or `--version` Release version (default value: `v1.0.0`)
- `-o` or `--output-format` Output format (default vale: `MARKDOWN`). Currently it supports `MARKDOWN` or `HTML` formats.

An example list of parameters:

`--user=joe --repo=example --since=4c087123 --until=b5f64456 --file=release_notes_example --output-format=MARKDOWN`

## Execution of the application

### How to execute the app in the command line

You need to have installed at least Java 17 and `JAVA_HOME` env variable should point to that Java binary file. 

```shell
$./gradlew bootJar

BUILD SUCCESSFUL in 2s
4 actionable tasks: 1 executed, 3 up-to-date
```

```shell
chmod u+x build/libs/releasenotes-0.0.1-SNAPSHOT.jar
```

```shell
java -jar build/libs/releasenotes-0.0.1-SNAPSHOT.jar --user=joe --repo=example --since=4c087123 --until=b5f64456 --file=release_notes_example --output-format=MARKDOWN
```

## How to execute the app from IntelliJ

1. Create a run configuration for the app under the Spring Boot option
2. Select Java 17 under the `Build and run` option
3. In the `Modify options` dropdown add `Program arguments`
4. Into the `Program arguments` field add the parameters for executing the app.
For example: `--user=joe --repo=example --since=4c087123 --until=b5f64456 --file=release_notes_example --output-format=MARKDOWN`
5. Click on the run button in the toolbar or select the `Run..` command in the `Run` menu and select the configuration you would like to execute.

The `release_notes_example.md` file is going to be generated into the project root in the selected `MARKDOWN` format.
The above defined parameters would create it for the `example` repository of `joe` user from the `4c087123` commit hash
until the `b5f64456` commit hash.


## TODO
1. [Add organisation option](https://github.com/ebi-gene-expression-group/release-notes-generator/issues/3)
2. [Add an option to use ENV files for parameters](https://github.com/ebi-gene-expression-group/release-notes-generator/issues/5)