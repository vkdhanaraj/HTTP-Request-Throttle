# HTTP-Request-Throttle


# Abstract 
Rate Limiting at server side is an effective approach to prevent infrastructure attacks as well as other suspicious activities. It allows better flow of data and increases security. Server is protected from being overloaded and high quality of services to the clients is maintained. Prevention of denial of services is achieved by throttling the requests. However, this project deals with client side Rate Limiting with intentions to enhance and optimize the performance of the application and device on which it runs.


# Introduction

There can be two types of rate limiting - Server side and client side. Server side rate limiting is done to protect server from heavy load. Client side rate limiting is mostly done to enhance the experience of the end user. This project focuses on client side rate limiting which means the rate limiting is done per application basis. Ex: When a mobile app is running on mobile - we can set max number of concurrent requests to be 5 which makes sure that only 5 requests are active at any moment irrespective of application user behavior.  

This project aims to provide developers a library using which he/she can rate limit the number of API calls made by the application. He can control the rate at which the requests are processed. 

