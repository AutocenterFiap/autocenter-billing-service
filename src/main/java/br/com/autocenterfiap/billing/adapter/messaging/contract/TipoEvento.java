package br.com.autocenterfiap.billing.adapter.messaging.contract;

/**
 * Valores do campo {@code tipo} do envelope, conforme o contrato da Saga.
 *
 * <p>O Billing consome 04, 07, 19 e 22 e publica 05, 06, 08, 09, 15, 16, 17, 20 e 21.
 */
public final class TipoEvento {

	private TipoEvento() {
	}

	// --- Consumidos pelo Billing (billing.comandos e os.eventos) -----
	public static final String BILLING_GERAR_ORCAMENTO = "billing.gerar-orcamento";
	public static final String BILLING_GERAR_COBRANCA = "billing.gerar-cobranca";
	public static final String BILLING_ESTORNAR_PAGAMENTO = "billing.estornar-pagamento";
	public static final String OS_CANCELADA = "os.cancelada";

	// --- Publicados pelo Billing (billing.eventos) -------------------
	public static final String ORCAMENTO_GERADO = "orcamento.gerado";
	public static final String ORCAMENTO_APROVADO = "orcamento.aprovado";
	public static final String ORCAMENTO_REPROVADO = "orcamento.reprovado";
	public static final String ORCAMENTO_FALHOU = "orcamento.falhou";
	public static final String PAGAMENTO_LINK_GERADO = "pagamento.link-gerado";
	public static final String PAGAMENTO_CONFIRMADO = "pagamento.confirmado";
	public static final String PAGAMENTO_RECUSADO = "pagamento.recusado";
	public static final String PAGAMENTO_ESTORNADO = "pagamento.estornado";
	public static final String PAGAMENTO_ESTORNO_FALHOU = "pagamento.estorno-falhou";
}

