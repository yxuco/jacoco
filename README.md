JaCoCo Java Code Coverage Library
=================================

[![Build Status](https://travis-ci.org/jacoco/jacoco.svg?branch=master)](https://travis-ci.org/jacoco/jacoco)
[![Build status](https://ci.appveyor.com/api/projects/status/g28egytv4tb898d7/branch/master?svg=true)](https://ci.appveyor.com/project/JaCoCo/jacoco/branch/master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.jacoco/org.jacoco.core/badge.svg?style=flat)](http://search.maven.org/#search|ga|1|g%3Aorg.jacoco)

JaCoCo is a free Java code coverage library distributed under the Eclipse Public
License. Check the [project homepage](http://www.eclemma.org/jacoco)
for downloads, documentation and feedback.

This fork is based on the version 0.7.7.201606060606.  It added method-level "include, exclude" to coverage report for groups. To use it, edit ant build xml for report, and add the following attributes to a group tag: include="RegEx for included methods" or exclude="RegEx for excluded methods". For example,
````
<structure name="FraudDetectionCache">
	<group name="Rules" include="execute">
		<classfiles>
			<fileset dir="${jacoco.work}/tmp/fdcache/FraudDetectionCache/be">
				<include name="be/gen/Rules/**/*.class"/>
			</fileset>
		</classfiles>
	</group>
	<group name="Functions" exclude="&lt;init&gt;">
		<classfiles>
			<fileset dir="${jacoco.work}/tmp/fdcache/FraudDetectionCache/be">
				<include name="be/gen/RuleFunctions/**/*$.class"/>
			</fileset>
		</classfiles>
	</group>
</structure>
````

This fork also added an ant report tag for generating code coverage reports on TIBCO BusinessWorks applications.  For example,
````
<jacoco:report>
	<businessworks name="MyBWApp" jmxurl="localhost:13401"/>
	<html destdir="${result.report.dir}" footer="TIBCO BusinessWorks Center of Excellence" />
</jacoco:report>
````
Note: We do not answer general questions in the project's issue tracker. Please contact the author for details of using this fork to generate code coverage reports for TIBCO BusinessWorks and BusinessEvents applications.
-------------------------------------------------------------------------
