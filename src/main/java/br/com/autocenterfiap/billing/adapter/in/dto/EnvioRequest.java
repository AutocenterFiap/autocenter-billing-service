package br.com.autocenterfiap.billing.adapter.in.dto;

import br.com.autocenterfiap.billing.domain.enums.TipoEnvio;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record EnvioRequest(
		@NotNull(message = "Tipo de envio é obrigatório")
		@Schema(
				description = "Tipo de envio: EMAIL, WHATSAPP, IMPRESSAO",
				example = "EMAIL",
				requiredMode = Schema.RequiredMode.REQUIRED,
				allowableValues = {"EMAIL", "WHATSAPP", "IMPRESSAO"}
		)
		TipoEnvio tipo) {
}

