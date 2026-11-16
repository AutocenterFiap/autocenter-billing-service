# language: pt
Funcionalidade: Orcamento e Pagamento (Billing)
  Como o servico de Billing
  Quero gerar orcamentos, registrar aprovacoes e processar cobrancas via Mercado Pago
  Para liberar a execucao do reparo e notificar o servico de Ordem de Servico

  Cenario: Fluxo completo de orcamento aprovado e pagamento confirmado
    Dado que o servico de OS solicita a geracao de orcamento para a OS 4242 no valor de "165.00"
    Entao o orcamento da OS 4242 fica com status "AGUARDANDO_APROVACAO"
    E o evento "orcamento.gerado" e publicado para a OS 4242
    Quando o cliente aprova o orcamento da OS 4242
    Entao o orcamento da OS 4242 fica com status "APROVADO"
    E o evento "orcamento.aprovado" e publicado para a OS 4242
    Quando o servico de OS solicita a geracao de cobranca para a OS 4242 no valor de "165.00"
    Entao o pagamento da OS 4242 fica com status "PENDENTE"
    E o evento "pagamento.link-gerado" e publicado para a OS 4242
    Quando o pagamento da OS 4242 e confirmado
    Entao o pagamento da OS 4242 fica com status "CONFIRMADO"
    E o evento "pagamento.confirmado" e publicado para a OS 4242

