package br.com.autocenterfiap.billing.adapter.out.mercadopago;

import br.com.autocenterfiap.billing.application.port.out.MercadoPagoPort;
import br.com.autocenterfiap.billing.infrastructure.config.MercadoPagoProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentRefundClient;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.core.MPRequestOptions;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.payment.PaymentRefund;
import com.mercadopago.resources.preference.Preference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MercadoPagoSdkAdapterTest {

	@Mock
	private PreferenceClient preferenceClient;

	@Mock
	private PaymentClient paymentClient;

	@Mock
	private PaymentRefundClient paymentRefundClient;

	@Mock
	private HttpClient httpClient;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void deveOperarEmModoStubQuandoTokenAusenteOuModoStub() {
		MercadoPagoProperties stubProps = new MercadoPagoProperties(
				"stub",
				"https://api.mercadopago.com",
				"",
				"",
				"",
				"http://localhost:4040/api/tunnels",
				"https://httpbin.org/get?s=1",
				"https://httpbin.org/get?f=1",
				"https://httpbin.org/get?p=1");

		MercadoPagoSdkAdapter adapter = new MercadoPagoSdkAdapter(stubProps, objectMapper);

		MercadoPagoPort.PreferenciaGerada pref = adapter.criarPreferenciaCheckoutPro(
				90L, 10L, 50L, new BigDecimal("165.00"), 1, Instant.now());
		assertThat(pref.preferenceId()).isEqualTo("pref-os-10-pag-90-t1");
		assertThat(pref.urlCheckout()).contains("os-10-t1");

		assertThat(adapter.consultarPagamento("123")).isEmpty();

		MercadoPagoPort.ResultadoEstorno estorno = adapter.estornarPagamento(
				90L, "123", new BigDecimal("165.00"));
		assertThat(estorno.sucesso()).isTrue();
		assertThat(estorno.refundId()).isEqualTo("refund-stub-90");
	}

	@Test
	@SuppressWarnings("unchecked")
	void deveCriarPreferenciaConsultarPagamentoEEstornarEmModoRealComNgrok() throws Exception {
		MercadoPagoProperties realProps = new MercadoPagoProperties(
				"real",
				"https://api.mercadopago.com",
				"TEST-PUB",
				"TEST-TOKEN",
				"",
				"http://localhost:4040/api/tunnels",
				"https://httpbin.org/get?s=1",
				"https://httpbin.org/get?f=1",
				"https://httpbin.org/get?p=1");

		HttpResponse<String> ngrokResponse = mock(HttpResponse.class);
		when(ngrokResponse.statusCode()).thenReturn(200);
		when(ngrokResponse.body()).thenReturn("""
				{
				  "tunnels": [
				    { "public_url": "http://meu-tunel.ngrok-free.app" },
				    { "public_url": "https://meu-tunel.ngrok-free.app/" }
				  ]
				}
				""");
		when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
				.thenReturn(ngrokResponse);

		Preference preference = mock(Preference.class);
		when(preference.getId()).thenReturn("pref-real-90");
		when(preference.getInitPoint()).thenReturn("https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=pref-real-90");
		when(preferenceClient.create(any(PreferenceRequest.class))).thenReturn(preference);

		MercadoPagoSdkAdapter adapter = new MercadoPagoSdkAdapter(
				realProps, preferenceClient, paymentClient, paymentRefundClient, objectMapper, httpClient);

		MercadoPagoPort.PreferenciaGerada gerada = adapter.criarPreferenciaCheckoutPro(
				90L, 10L, 50L, new BigDecimal("165.00"), 1, Instant.parse("2026-01-01T00:30:00Z"));

		assertThat(gerada.preferenceId()).isEqualTo("pref-real-90");
		assertThat(gerada.urlCheckout()).contains("pref-real-90");

		ArgumentCaptor<PreferenceRequest> prefCaptor = ArgumentCaptor.forClass(PreferenceRequest.class);
		verify(preferenceClient).create(prefCaptor.capture());
		assertThat(prefCaptor.getValue().getExternalReference()).isEqualTo("90");
		assertThat(prefCaptor.getValue().getNotificationUrl())
				.isEqualTo("https://meu-tunel.ngrok-free.app/v1/pagamentos/webhook/mercadopago");

		// Consulta de pagamento
		Payment payment = mock(Payment.class);
		when(payment.getId()).thenReturn(777L);
		when(payment.getExternalReference()).thenReturn("90");
		when(payment.getStatus()).thenReturn("approved");
		when(payment.getStatusDetail()).thenReturn("accredited");
		when(payment.getTransactionAmount()).thenReturn(new BigDecimal("165.00"));
		when(paymentClient.get(777L)).thenReturn(payment);

		Optional<MercadoPagoPort.DadosPagamentoExterno> consultado = adapter.consultarPagamento("777");
		assertThat(consultado).isPresent();
		assertThat(consultado.get().aprovado()).isTrue();
		assertThat(consultado.get().externalReferencePagamentoId()).isEqualTo(90L);

		// Estorno com sucesso
		PaymentRefund refund = mock(PaymentRefund.class);
		when(refund.getId()).thenReturn(555L);
		when(paymentRefundClient.refund(eq(777L), eq(new BigDecimal("165.00")), any(MPRequestOptions.class)))
				.thenReturn(refund);

		MercadoPagoPort.ResultadoEstorno resultadoEstorno =
				adapter.estornarPagamento(90L, "777", new BigDecimal("165.00"));
		assertThat(resultadoEstorno.sucesso()).isTrue();
		assertThat(resultadoEstorno.refundId()).isEqualTo("555");

		// Estorno com falha na operadora
		when(paymentRefundClient.refund(eq(888L), any(), any(MPRequestOptions.class)))
				.thenThrow(new RuntimeException("Timeout operadora"));
		MercadoPagoPort.ResultadoEstorno falhaEstorno =
				adapter.estornarPagamento(91L, "888", new BigDecimal("165.00"));
		assertThat(falhaEstorno.sucesso()).isFalse();
		assertThat(falhaEstorno.motivoErro()).contains("Operadora recusou o estorno");

		// Expiracao de preferencia no Checkout Pro
		adapter.expirarPreferencia("pref-real-90");
		verify(preferenceClient).update(eq("pref-real-90"), any(PreferenceRequest.class));
	}
}

