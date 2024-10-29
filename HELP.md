# Getting Started

### Reference Documentation

# Personal comments

* The project is documented using swagger
* To access the swagger documentation please enter: http://localhost:8081/swagger-ui/index.html
* In a normal project I would implement exception handling using @ControllerAdvice but it seemed too much for a small
  demo
* I dealt with "new coins" using a whitelist found in application.properties. Normally I would put that in a database.
* I would probably also cache some common results for performance reasons, such as the normalized range for the most
* popular coins in the last 24 hours, and maybe for the last week/month/year.
* The project uses a gateway for rate limiting. I had problems with postman testing but it can be done from curl with
  this script:
* ```` for i in {1..11}; do
  curl -i http://localhost:8080/api/coins/normalized-range
  echo "Request #$i" 
  done

* I haven't written unit tests in a long, long time. If you're reading this before the unit tests, sorry in advance
* for that horror show.

# How to start the app

# how to access the swagger file

* To access the swagger documentation please enter: http://localhost:8081/swagger-ui/index.html

