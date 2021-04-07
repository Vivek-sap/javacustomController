# [ Java Custom Controller ]

Counter service/db suffice following usecase
- Runs in the same cluster as the REST server in the first step
- Reconciles on the Deployments created with label: `app=nginx` in any namespace 
- When having n deployments in the cluster with the given label, the counter service should
return n via the implemented REST endpoint.

### Technology Used

* Spring boot
* Java 8 
* io.fabric8

### Rest endpoints
Not exposed to Outside

### Usage

##### Compile
```
mvn clean install
mvn package

```

#####  Docker image generation
```
Example

docker build -t springboot-k8s-controller:v1.0 .

```
### Deployment

All the yaml file are defined in deployment folder of the project. 

##### prerequisite
minikube or any other kubernetes tool install locally. Run the script accordingly 


```
eval $(minikube docker-env)

Configmap Deployment
--------------------
kubectl apply -f counter-configmap.yml
kubectl apply -f fabric8-rbac.yml

Create Custom Resource Type
--------------------
kubectl apply -f customresource.yml
kubectl apply -f podset.yml

Deployment
--------------------
kubectl apply -f controller-deployment.yml
```



### Improvement
Missing Test case. 

