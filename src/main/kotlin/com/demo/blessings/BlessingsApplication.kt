package com.demo.blessings

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.http.HttpInputMessage
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.xml.ws.Response

@SpringBootApplication
class BlessingsApplication

fun main(args: Array<String>) {
	Database.connect(url = "jdbc:mysql://mysql:3306/BLESSINGS",
			driver = "com.mysql.cj.jdbc.Driver",
			user = "blessUser",
			password = "bl355")
	runApplication<BlessingsApplication>(*args)
}

object Bless: Table() {
	val id = integer(name = "id").autoIncrement().primaryKey()
	val message = varchar(length = 60, name = "message")
}

data class BlessResponse(val id: Int, val message: String)

object BlessMap {
	fun toBlessResponse(row: ResultRow) =
			BlessResponse(id = row[Bless.id], message = row[Bless.message] )
}


@RestController
@RequestMapping("/v1/blessings")
class BlessController {

	@GetMapping("")
	fun getAll(): ResponseEntity<List<BlessResponse>> {
		return transaction {
			Bless.selectAll().map {
				BlessMap.toBlessResponse(it)
			}.run {
				ResponseEntity.ok().body(this)
			}
		}
	}

}
