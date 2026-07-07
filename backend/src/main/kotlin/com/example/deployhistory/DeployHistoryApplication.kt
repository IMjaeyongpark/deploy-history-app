package com.example.deployhistory

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DeployHistoryApplication

fun main(args: Array<String>) {
    runApplication<DeployHistoryApplication>(*args)
}
