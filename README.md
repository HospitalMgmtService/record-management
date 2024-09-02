# Record Management Service
## This microservice is responsible for:
1. Develop Electronic Health Records (EHR) System:
   * Implement a system to digitize patient medical records, ensuring easy access and efficient management.
   * Develop real-time data synchronization to ensure EHRs are consistently updated with new medical information.

2. Implement Security and Privacy Controls:
   * Design and enforce access control mechanisms to restrict record access to authorized personnel only.
   * Develop features to ensure compliance with healthcare regulations, such as HIPAA, including encryption, secure data transmission, and storage.

3. Enable Secure Access and Monitoring:
   * Create a secure access management system for EHRs, including authentication and role-based access controls.
   * Implement audit trail functionality to log and monitor all access and changes to patient records for security and accountability.

## Tech stack
* Build tool: maven >= 3.8.8
* Java: 21
* Framework: Spring boot 3.2.x
* DBMS: Postgresql

## Prerequisites
* Java SDK 21
* PGAdmin server2

## Format code
`mvn spotless:apply`

## JaCoCo report
`mvn clean test jacoco:report`

## SonarQube
* `docker pull sonarqube:lts-community`
* `docker run --name sonar-qube -p 9000:9000 -d sonarqube:lts-community`
* `mvn clean verify sonar:sonar -Dsonar.projectKey=pnk.identity.master -Dsonar.host.url=http://localhost:9000 -Dsonar.login=CHANGE_ME`

## Start application
`mvn spring-boot:run`

## Build application
`mvn clean package`

## Docker guideline
### Build docker image
`docker build -t record-management:0.9 .`

### Tag the image
`docker tag record-management:0.9 <DOCKER_ACCOUNT>/record-management:0.9`

### Push docker image to Docker Hub
`docker push <DOCKER_ACCOUNT>/record-management:0.9`

### Run the built image
`docker run -d --name record-management -p 9192:9192 record-management:0.9`

### Create network
`docker network create pnk-network`

### Start Postgresql in pnk-network
`docker run --network pnk-network --name postgresql -p 5432:5432 -e POSTGRESQL_ROOT_PASSWORD=root -d postgresql:8.0.36-debian`

### Run your application in pnk-network
`docker run --name record-management --network pnk-network -p 9192:9192 -e DBMS_CONNECTION=jdbc:postgresql://localhost:5432/hospital record-management:0.9`
