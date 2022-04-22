package org.kie.jenkins.jobdsl

import groovy.json.JsonOutput

import org.kie.jenkins.jobdsl.templates.KogitoJobTemplate
import org.kie.jenkins.jobdsl.KogitoConstants
import org.kie.jenkins.jobdsl.Utils
import org.kie.jenkins.jobdsl.VersionUtils

/**
* Seed Job utils
* 
* Common methods to create seed jobs
**/
class SeedJobUtils {

    static def createSeedJobTrigger(def script, String jobName, String repository, String gitAuthor, String gitBranch, List pathsToListen, String jobRelativePathToTrigger) {
        if (pathsToListen.isEmpty()) {
            throw new RuntimeException('pathsToListen cannot be empty, else it would end up in an infinite loop ...');
        }
        def job = script.pipelineJob(jobName) {
            description('This job listens to pipelines repo and launch the seed job if needed. DO NOT USE FOR TESTING !!!! See https://github.com/kiegroup/kogito-pipelines/blob/main/docs/jenkins.md#test-specific-jobs')

            logRotator {
                numToKeep(5)
            }

            throttleConcurrentBuilds {
                maxTotal(1)
            }

            parameters {
                booleanParam('FORCE_REBUILD', false, 'Default, the job will scan for modified files and do the update in case some files are modified. In case you want to force the DSL generation')
            }

            environmentVariables {
                env('JOB_RELATIVE_PATH_TO_TRIGGER', jobRelativePathToTrigger)
                env('LISTEN_TO_MODIFIED_PATHS', new groovy.json.JsonBuilder(pathsToListen).toString())
            }

            definition {
                cpsScm {
                    scm {
                        git {
                            remote {
                                url("https://github.com/${gitAuthor}/${repository}.git")
                                credentials('kie-ci')
                            }
                            branch(gitBranch)
                            extensions {
                                cleanBeforeCheckout()
                            }
                        }
                    }
                    scriptPath('dsl/seed/jobs/Jenkinsfile.seed.trigger')
                }
            }

            properties {
                githubProjectUrl("https://github.com/${gitAuthor}/${repository}/")

                pipelineTriggers {
                    triggers {
                        gitHubPushTrigger()
                    }
                }
            }
        }
        // Trigger jobs need to be executed once for the hook to work ...
        // There will 
        script.queue(jobName)
        return job
    }
}