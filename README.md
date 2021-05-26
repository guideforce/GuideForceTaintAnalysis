# GuideForce

A common approach to improve software quality is to use programming guidelines to avoid common kinds of errors. This tool is **a static analyzer for enforcing programming guidelines to Java programs**. It takes a Java bytecode program and a programming guideline as inputs, infers the trace information of the program and then verifies if it is accepted by the guideline. If the inferred result is not acceptable, the tool tries to search for an execution path of the program that violates the guideline.

The [previous version](https://github.com/ezal/TSA) of the tool was developed by Zălinescu et al., based on their [APLAS'2017 paper](https://doi.org/10.1007/978-3-319-71237-6_5).

## Authors
- [Ulrich Schöpp](https://ulrichschoepp.de/)
- [Chuangjie Xu](https://cj-xu.github.io/)
- Jakob Knauer

## New Features
The new features of the current version include:
- It enhaces the analysis precision by capturing the effect for each region where a value may locate.
- Effect annotations are given by the abstract interpretation based on Büchi automata due to [Hofmann and Chen](https://doi.org/10.1145/2603088.2603127).
- It can analyze also nonterminating programs by capturing the information of their infinite traces.
- It supports exception handling, i.e., it can analyze programs with exceptions.
- It tries to report a counterexample when it judges that the program may not adhere to the guideline.

## Prerequisites
- Java 8
- [Soot](http://soot-oss.github.io/soot/)
- JUnit (tested with version 4.13)
- SLF4J (tested with version 1.7.30)

## Building
The tool can be built using [Gradle](https://gradle.org/). A configuration file "[build.gradle](https://github.com/cj-xu/GuideForceJava/blob/main/build.gradle)" is provided in the repository. For example, in an IDE (e.g. [IntelliJ IDEA](https://www.jetbrains.com/idea/)), one can setup a project for the tool by opening the build.gradle file as a project.

## Experiment
The tool was tested and evaluated with the [Securibench Micro](http://too4words.github.io/securibench-micro/) benchmark as well as a bunch of additional examples.

The benchmark provides 122 test cases in 12 categories. Each benchmark program implements a small self-contained servlet. Our tool are applied to verify if they adhere to the [guideline](https://github.com/cj-xu/GuideForceJava/blob/main/src/main/java/guideforce/policy/BinaryPolicy.java) that only untainted commands can be executed.

The [testcases](https://github.com/cj-xu/GuideForceJava/tree/main/src/test/java/testcases) folder contains 19 programs with typical nonterminating behaviors and 10 programs with exception handling for testing the infinitary and exceptional effect analysis. For these programs, a [simple policy](https://github.com/cj-xu/GuideForceJava/blob/main/src/main/java/guideforce/policy/ABCPolicy.java) is employed to test if the tool computes the correct effects.

There are totally 157 test cases (see the [Evaluation.java](https://github.com/cj-xu/GuideForceJava/blob/main/src/test/java/guideforce/Evaluation.java) file) and 145 of them run as expected. The tool does not support features such as reflection and concurrency, resulting in 8 failed tests. The remaining 4 failures are due to the limitation of the approach (for example, the analysis is not fully path-sensitivity).

The experiment can be run from the commond-line by typing:
```
./gradlew build
```
