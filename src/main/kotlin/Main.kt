import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

fun main(args: Array<String>) {
    // Lista de arquivos JSON para ler
    val jsonFiles = listOf("dev.json", "hom.json", "prod.json")

    // Crie o diretório de saída se ele não existir
    val outputDir = Paths.get(System.getProperty("user.dir"), "env-results")
    if (!Files.exists(outputDir)) {
        Files.createDirectory(outputDir)
    }

    // Crie um ObjectMapper
    val mapper: ObjectMapper = jacksonObjectMapper()

    // Percorra cada arquivo JSON
    for (file in jsonFiles) {
        // Carregue o arquivo JSON
        val jsonFile = File(Paths.get(System.getProperty("user.dir"), "env", file).toString()).readText()
        val jsonData: JsonNode = mapper.readTree(jsonFile).get("Parametros")

        // Carregue o arquivo YAML
        val yamlFile = File(Paths.get(System.getProperty("user.dir"), "env", "app.yml").toString()).readText()
        var yamlLines = yamlFile.lines().toMutableList()

        // Percorra cada linha do arquivo YAML
        for (i in yamlLines.indices) {
            if (yamlLines[i].contains("\${")) {
                // Encontre a variável de ambiente
                val envVar = Regex("""\$\{(.*)}""").find(yamlLines[i])?.groups?.get(1)?.value

                // Substitua pela propriedade correspondente do arquivo JSON
                if (envVar == "IMG") {
                    yamlLines[i] = yamlLines[i].replace("\${$envVar}", "IMG")
                } else if (jsonData.has(envVar)) {
                    yamlLines[i] = yamlLines[i].replace("\${$envVar}", jsonData.get(envVar).asText())
                }
            }
        }

        // Junte as linhas atualizadas de volta em uma string
        val updatedYaml = yamlLines.joinToString("\n")

        // Crie um subdiretório para o ambiente atual se ele não existir
        val envDir = Paths.get(outputDir.toString(), file.substringBefore(".json"))
        if (!Files.exists(envDir)) {
            Files.createDirectory(envDir)
        }

        // Escreva o YAML atualizado para um novo arquivo no subdiretório do ambiente
        File(Paths.get(envDir.toString(), "kubernetes.yml").toString()).writeText(updatedYaml)
    }

    println("Arquivos gerados kubernetes gerados na pasta env-results!!!")
}
