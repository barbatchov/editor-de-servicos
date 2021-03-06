package br.gov.servicos.editor.frontend;
/***/

import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import static lombok.AccessLevel.PRIVATE;

@Slf4j
@Component
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class VCGE {

    public static final String URL = "https://raw.githubusercontent.com/govbr/vocab-govbr/3ad9daf81cfc793f6a5b87e5daecf17a3195579c/id/vcge.json";

    RestTemplate rest;

    @Autowired
    public VCGE(@Qualifier("restTemplate") RestTemplate rest) {
        this.rest = rest;
    }

    @Cacheable("vcge")
    public String get() {
        log.info("Requisitando estrutura do VCGE...");
        try {
            return rest.getForEntity(URL, String.class).getBody();
        } catch (Exception e) {
            log.error("Erro ao requisitar lista de categorias do VCGE", e);
            throw e;
        }
    }
}
