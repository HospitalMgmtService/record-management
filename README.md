# Record Management Service
## This microservice is responsible for:
* Electronic Health Records (EHR)
* Digitize patient medical records for easy access and management
* Ensure EHRs are updated in real-time with new medical information
* Security and Privacy
* Implement access controls to ensure only authorized personnel can view or update records
* Ensure compliance with healthcare regulations like HIPAA (Health Insurance Portability and Accountability Act)
* Access and Updates
* Provide secure access to EHRs for authorized staff
* Implement audit trails to track changes made to patient records

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
