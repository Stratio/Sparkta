/**
 * Copyright (C) 2015 Stratio (http://stratio.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.stratio.sparkta.serving.core

/**
 * Global constants of the application.
 */
object AppConstant {

  final val JarPluginsFolder = "plugins"
  final val ClasspathJarFolder = "cluster"
  final val ClusterExecutionJarFolder = "driver"
  final val ExecutionMode = "executionMode"
  final val DefaultExecutionMode = "local"
  final val ConfigAppName = "sparkta"
  final val ConfigApi = "api"
  final val ConfigHdfs = "hdfs"
  final val ConfigDetail = "config"
  final val ConfigLocal = "local"
  final val ConfigStandAlone = "standAlone"
  final val ConfigMesos = "mesos"
  final val ConfigYarn = "yarn"
  final val ConfigAkka = "akka"
  final val ConfigSwagger = "swagger"
  final val ConfigZookeeper = "zk"
  final val BaseZKPath = "/stratio/sparkta"
  final val PoliciesBasePath = s"${AppConstant.BaseZKPath}/policies"
  final val ContextPath = s"${AppConstant.BaseZKPath}/contexts"
  final val ConfigRememberPartitioner = "rememberPartitioner"
  final val ConfigStopGracefully = "stopGracefully"

  //Hdfs Options
  final val HadoopUserName = "hadoopUserName"
  final val HadoopConfDir = "hadoopConfDir"
  final val HdfsMaster = "hdfsMaster"
  final val PluginsFolder = "pluginsFolder"
  final val ClasspathFolder = "classpathFolder"
  final val ExecutionJarFolder = "executionJarFolder"

  //Generic Options
  final val DeployMode = "deployMode"
  final val NumExecutors = "numExecutors"
  final val TotalExecutorCores = "totalExecutorCores"
  final val ExecutorMemory = "executorMemory"
  final val ExecutorCores = "executorCores"
  final val SparkHome = "sparkHome"

  //Mesos Options
  final val MesosMasterDispatchers = "masterDispatchers"

  //StandAlone
  final val StandAloneSupervise = "supervise"
  final val StandAloneMasterNode = "master"

  //Yarn
  final val YarnQueue = "queue"
  final val YarnMaster = "master"

  //Zookeeper
  final val ZookeeperConnection = "connectionString"
  final val DefaultZookeeperConnection = "localhost:2181"
  final val ZookeeperConnectionTimeout = "connectionTimeout"
  final val DefaultZookeeperConnectionTimeout = 15000
  final val ZookeeperSessionTimeout = "sessionTimeout"
  final val DefaultZookeeperSessionTimeout = 60000
  final val ZookeeperRetryAttemps = "retryAttempts"
  final val DefaultZookeeperRetryAttemps = 5
  final val ZookeeperRetryInterval = "retryInterval"
  final val DefaultZookeeperRetryInterval = 10000

  //HDFS
  final val DefaultHadoopUserName = "stratio"
}
