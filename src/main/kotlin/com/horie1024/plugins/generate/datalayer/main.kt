import com.horie1024.plugins.generate.datalayer.lib.DataLayerGenerator
import io.swagger.models.Swagger
import io.swagger.parser.SwaggerParser

fun main(args: Array<String>) {

    // SwaggerオブジェクトからAPI仕様にアクセス
    val swagger: Swagger = SwaggerParser().read("http://petstore.swagger.io/v2/swagger.json")
    //val swagger: Swagger = SwaggerParser().read("swagger.json")

    DataLayerGenerator(swagger).run {
        generateDataClass()
        generateRetrofitInterface()
    }
}