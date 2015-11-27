package br.gov.servicos.editor.usuarios;

import br.gov.servicos.editor.usuarios.cadastro.CamposSenha;
import br.gov.servicos.editor.usuarios.cadastro.TokenRecuperacaoSenhaRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static br.gov.servicos.editor.usuarios.TokenError.EXPIRADO;
import static br.gov.servicos.editor.usuarios.TokenError.INVALIDO;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Optional.empty;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RecuperacaoSenhaServiceTest {

    private static final String TOKEN = "token";
    private static final java.lang.String ENCRYPTED_TOKEN = "encrypted";
    private static final Long USUARIO_ID = 12341234L;
    private static final String SENHA = "12341234";
    private static final String ENCRYPTED_SENHA = "******";
    private static final Long TOKEN_ID = 1L;
    public static final int MAX = 10;

    @Mock
    private GeradorToken geradorToken;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenRecuperacaoSenhaRepository repository;

    @Mock
    private RecuperacaoSenhaValidator validator;

    @Mock
    private UsuarioService usuarioService;

    @InjectMocks
    private RecuperacaoSenhaService recuperacaoSenhaService;

    @Before
    public void setUp() {
        when(passwordEncoder.encode(TOKEN)).thenReturn(ENCRYPTED_TOKEN);
        when(passwordEncoder.encode(SENHA)).thenReturn(ENCRYPTED_SENHA);
        ReflectionTestUtils.setField(recuperacaoSenhaService, "maxTentativasToken", MAX);
    }


    @Test
    public void deveGerarTokenAleatorioEGuardarEncryptadoNoBanco() {
        when(geradorToken.gerar()).thenReturn(TOKEN);
        String token = recuperacaoSenhaService.gerarTokenParaUsuario(USUARIO_ID.toString());

        TokenRecuperacaoSenha expectedTokenRecuperacaoSenha = new TokenRecuperacaoSenha()
                .withToken(ENCRYPTED_TOKEN)
                .withUsuario(new Usuario().withId(USUARIO_ID))
                .withTentativasSobrando(MAX);
        verify(repository).save(refEq(expectedTokenRecuperacaoSenha, "dataCriacao"));
        assertThat(token, equalTo(TOKEN));
    }


    @Test
    public void deveDesabilitarUsuarioQuandogerarToken() {
        recuperacaoSenhaService.gerarTokenParaUsuario(USUARIO_ID.toString());
        verify(usuarioService).desabilitarUsuario(USUARIO_ID.toString());
    }

    @Test
    public void deveSalvarSenhaSeTokenForValido() throws TokenInvalido {
        FormularioRecuperarSenha formulario = criarFormulario(USUARIO_ID, SENHA);
        Usuario usuario = new Usuario();
        TokenRecuperacaoSenha token = new TokenRecuperacaoSenha().withUsuario(usuario);

        when(repository.findByUsuarioIdOrderByDataCriacaoAsc(USUARIO_ID)).thenReturn(newArrayList(token));

        when(validator.hasError(formulario, token)).thenReturn(empty());

        recuperacaoSenhaService.trocarSenha(formulario);
        verify(usuarioService).save(usuario.withSenha(ENCRYPTED_SENHA));
    }

    @Test
    public void deveDeletarTokenCasoSenhaTenhaSidoTrocada() throws TokenInvalido {
        FormularioRecuperarSenha formulario = criarFormulario(USUARIO_ID, SENHA);
        Usuario usuario = new Usuario();
        TokenRecuperacaoSenha token = new TokenRecuperacaoSenha()
                .withUsuario(usuario)
                .withId(TOKEN_ID);
        when(repository.findByUsuarioIdOrderByDataCriacaoAsc(USUARIO_ID)).thenReturn(newArrayList(token));
        when(validator.hasError(formulario, token)).thenReturn(empty());

        recuperacaoSenhaService.trocarSenha(formulario);

        verify(repository).delete(TOKEN_ID);
    }

    @Test
    public void naoDeveSalvarSenhaSeTokenForInvalido() {
        FormularioRecuperarSenha formulario = criarFormulario(USUARIO_ID, SENHA);
        Usuario usuario = new Usuario();
        TokenRecuperacaoSenha token = new TokenRecuperacaoSenha().withUsuario(usuario).withTentativasSobrando(0);
        when(repository.findByUsuarioIdOrderByDataCriacaoAsc(USUARIO_ID)).thenReturn(newArrayList(token));
        when(validator.hasError(formulario, token)).thenReturn(Optional.of(INVALIDO));

        try {
            recuperacaoSenhaService.trocarSenha(formulario);
            fail();
        } catch (TokenInvalido e) {
            verify(usuarioService, never()).save(usuario.withSenha(ENCRYPTED_SENHA));
        }
    }

    @Test
    public void deveIncrementarNumeroDeTentativasSeTokenForInvalido() throws TokenInvalido {
        FormularioRecuperarSenha formulario = criarFormulario(USUARIO_ID, SENHA);
        Usuario usuario = new Usuario();
        TokenRecuperacaoSenha token = new TokenRecuperacaoSenha()
                .withUsuario(usuario)
                .withTentativasSobrando(MAX);
        TokenRecuperacaoSenha expectedToken = new TokenRecuperacaoSenha()
                .withUsuario(usuario)
                .withTentativasSobrando(MAX-1);

        when(repository.findByUsuarioIdOrderByDataCriacaoAsc(USUARIO_ID)).thenReturn(newArrayList(token));
        when(validator.hasError(formulario, token)).thenReturn(Optional.of(INVALIDO));

        try {
            recuperacaoSenhaService.trocarSenha(formulario);
        } catch (CpfTokenInvalido e) {
            verify(repository).save(expectedToken);
        }
    }


    @Test
    public void deveLancarExcecaoCasoTokenSejaInvalidoComMensagemDizendoQuantasTentativasEstaoFaltando() throws TokenInvalido {
        FormularioRecuperarSenha formulario = criarFormulario(USUARIO_ID, SENHA);
        Usuario usuario = new Usuario();
        TokenRecuperacaoSenha token = new TokenRecuperacaoSenha()
                .withUsuario(usuario)
                .withTentativasSobrando(MAX);

        when(repository.findByUsuarioIdOrderByDataCriacaoAsc(USUARIO_ID)).thenReturn(newArrayList(token));
        when(validator.hasError(formulario, token)).thenReturn(Optional.of(INVALIDO));

        try {
            recuperacaoSenhaService.trocarSenha(formulario);
            fail();
        } catch(CpfTokenInvalido e) {
            assertThat(e.getTentativasSobrando(), equalTo(MAX-1));
        }
    }

    @Test(expected = TokenExpirado.class)
    public void deveLancarExcecaoDeTokenExpiradoCasoTokenEstejaExpirado() throws TokenInvalido {
        FormularioRecuperarSenha formulario = criarFormulario(USUARIO_ID, SENHA);
        Usuario usuario = new Usuario();
        TokenRecuperacaoSenha token = new TokenRecuperacaoSenha()
                .withUsuario(usuario)
                .withTentativasSobrando(MAX);

        when(repository.findByUsuarioIdOrderByDataCriacaoAsc(USUARIO_ID)).thenReturn(newArrayList(token));
        when(validator.hasError(formulario, token)).thenReturn(Optional.of(EXPIRADO));

        recuperacaoSenhaService.trocarSenha(formulario);
    }

    @Test(expected = UsuarioInexistenteException.class)
    public void deveLancarExcecaoSeNaoEncontrarTokenParaUsuario() throws TokenInvalido {
        FormularioRecuperarSenha formulario = criarFormulario(USUARIO_ID, SENHA);
        when(repository.findByUsuarioIdOrderByDataCriacaoAsc(USUARIO_ID)).thenReturn(newArrayList());
        recuperacaoSenhaService.trocarSenha(formulario);
    }

    private FormularioRecuperarSenha criarFormulario(Long usuarioId, String senha) {
        CamposVerificacaoRecuperarSenha camposVerificacaoRecuperarSenha = new CamposVerificacaoRecuperarSenha()
                .withUsuarioId(usuarioId.toString());
        return new FormularioRecuperarSenha()
                    .withCamposVerificacaoRecuperarSenha(camposVerificacaoRecuperarSenha)
                    .withCamposSenha(new CamposSenha().withSenha(senha));
    }

}