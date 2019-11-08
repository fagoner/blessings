package com.demo.blessings

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest
class BlessingsApplicationTests {

	companion object{

		@JvmStatic
		@BeforeClass
		fun initialize() {
			Database.connect(url = "jdbc:mysql://mysql:3306/BLESSINGS",
					driver = "com.mysql.cj.jdbc.Driver",
					user = "blessUser",
					password = "bl355")
		}
	}


	@Test
	fun gettingAll() {
		transaction {
			Bless.select { Bless.id.eq(1) }
					.first()
					.run { assert(this[Bless.message] == "Have a nice day") }
		}
	}

}
