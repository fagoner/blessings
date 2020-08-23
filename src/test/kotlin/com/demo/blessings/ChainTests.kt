package com.demo.blessings

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

object Product: Table(name = "product") {
    val id = integer(name = "id").primaryKey().autoIncrement("productoIdSeqName")
    val name = varchar(name = "name", length = 40)
}

object ClientCategory: Table(name = "client_category") {
    val id = integer(name = "id").primaryKey().autoIncrement("clientIdSeqName")
    val name = varchar(name = "name", length = 40)
}

object ProductClientCategoryPrice: Table(name = "producto_cliente_categoria_precio"){
    val productId = (integer(name = "product_id") references (Product.id)).primaryKey(0)
    val clientCategoryId = (integer(name = "client_category_id") references (ClientCategory.id)).primaryKey(1)
    val price = integer(name = "price")
}

sealed class ProductModel {
    data class Dto(val id: Int, val name: String)
}

sealed class ClientCategoryModel {
    data class Dto(val id: Int, val name: String)
}

data class ClienteCategoryPriceRequest(val categoryClientId: Int, val price: Int)
data class ProductClienteCategoryPriceRequest(val list: List<ClienteCategoryPriceRequest>)

data class ProductClienteCategoryPriceModel(val productId: Int, val clientCategoryId: Int, val price: Int)

fun ResultRow.toProductModelDto(): ProductModel.Dto {
    return ProductModel.Dto(id = this[Product.id], name = this[Product.name])
}

fun ResultRow.toClienteCategoryModelDto(): ClientCategoryModel.Dto {
    return ClientCategoryModel.Dto(id = this[ClientCategory.id], name = this[ClientCategory.name])
}

fun ResultRow.toProductClienteCategoryPriceModel(): ProductClienteCategoryPriceModel{
    return ProductClienteCategoryPriceModel(
            productId = this[ProductClientCategoryPrice.productId],
            clientCategoryId = this[ProductClientCategoryPrice.clientCategoryId],
            price = this[ProductClientCategoryPrice.price]
    )
}

class ChainTests {

    companion object {
        @JvmStatic
        @BeforeClass
        fun init() {
            Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")

            transaction {

                SchemaUtils.create(Product, ClientCategory, ProductClientCategoryPrice)

                Product.insert { it[name] = "Product 1" }
                Product.insert { it[name] = "Product 2" }

                ClientCategory.insert { it[name] = "Category 1" }
                ClientCategory.insert { it[name] = "Category 2" }
            }
        }

        @JvmStatic
        @AfterClass
        fun tearDown() {

            println("all categories")
            transaction {
                ClientCategory.selectAll().forEach {
                    println(it.toClienteCategoryModelDto())
                }
            }

            println("all products")
            transaction {
                Product.selectAll().forEach {
                    println(it.toProductModelDto())
                }
            }
            println("all relations")
            transaction {
                ProductClientCategoryPrice.selectAll().forEach {
                    println(it.toProductClienteCategoryPriceModel())
                }
            }
            transaction { SchemaUtils.drop(Product, ClientCategory, ProductClientCategoryPrice) }
        }
    }

    @Test
    fun happyPathCreateProductClientCategoryPrice() {
        transaction {
            ProductClientCategoryPrice.insert {
                it[productId] = 1
                it[clientCategoryId]=1
                it[price] = 1
            }
        }
    }

    @Test
    fun badPathCreateProductClientCategoryPriceButProductIdDoesNotExists() {
        assertThatThrownBy {
            transaction {
                ProductClientCategoryPrice.insert {
                    it[productId] =0
                    it[clientCategoryId]=1
                    it[price] = 1
                }
            }
        }

    }

    @Test
    fun badPathAddProduct1CategoryClient2AndTryToRepeat() {
        transaction {
            ProductClientCategoryPrice.insert {
                it[productId] =1
                it[clientCategoryId]=2
                it[price] = 1
            }
        }

        assertThatThrownBy {
            transaction {
                ProductClientCategoryPrice.insert {
                    it[productId] = 1
                    it[clientCategoryId]= 2
                    it[price] = 1
                }
            }
        }
    }



    private fun insertProduct(name: String): Int {
        return Product.insert {
            it[Product.name] = name
        } get Product.id
    }

    private fun insertProductClientCategoryPrice(productIdx: Int, categoryClientIdx: Int, pricex: Int) {
        ProductClientCategoryPrice.insert {
            it[productId] = productIdx
            it[clientCategoryId] = categoryClientIdx
            it[price] = pricex
        }
    }

    @Test
    fun createNewProductAndTryToGivePriceForAnyCategory() {

        val goodPrices = ProductClienteCategoryPriceRequest(
         list = listOf(
                 ClienteCategoryPriceRequest(categoryClientId = 1, price = 10),
                 ClienteCategoryPriceRequest(categoryClientId = 2, price = 20)))

        var attemptProductId = 0

        transaction {
//            TransactionManager.current().rollback()
            attemptProductId = insertProduct("Failed demo")
            println("I'm NOT going to fail: $attemptProductId")
            goodPrices.list.forEach {
                item ->
                insertProductClientCategoryPrice(attemptProductId, item.categoryClientId, item.price)
            }
        }.apply {
            assertThat(
                    transaction {
                        val query = ProductClientCategoryPrice.select { ProductClientCategoryPrice.productId.eq(attemptProductId)}
                        query.forEach {
                            println(it.toProductClienteCategoryPriceModel())
                        }
                        assertThat(query.empty()).isFalse()
                    }
            )
        }

    }

    @Test
    fun createNewProductWithProductIdThatDoesNotExistsAndRaiseError() {

        val goodPrices = ProductClienteCategoryPriceRequest(
                list = listOf(
                        ClienteCategoryPriceRequest(categoryClientId = 0, price = 10),
                        ClienteCategoryPriceRequest(categoryClientId = 0, price = 20)))

        var attemptProductId = 0

        assertThatThrownBy {
            transaction {
                attemptProductId = insertProduct("Failed demo")
                println("I'm going to fail: $attemptProductId")

                goodPrices.list.forEach {
                    item ->
                    insertProductClientCategoryPrice(attemptProductId, item.categoryClientId, item.price)
                }
            }
        }


        assertThat(
                transaction {
                    val query = ProductClientCategoryPrice.select { ProductClientCategoryPrice.productId.eq(attemptProductId)}
                    assertThat(query.empty()).isTrue()
                }
        )
        println("attemptProductId::: ${attemptProductId}")

    }


}