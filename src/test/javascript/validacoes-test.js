'use strict';

var validador = require('validacoes');

describe('validação >', function () {
  describe('servico >', function () {
    it('deve ter nome válido', function () {
      expect(validador.nome('nome de testes')).toBeUndefined();
    });
    it('deve obrigar ter nome', function () {
      expect(validador.nome()).toBe('nome-obrigatorio');
      expect(validador.nome('')).toBe('nome-obrigatorio');
    });
    it('nome deve ter no máximo 150 caracteres', function () {
      expect(validador.nome(_.repeat('x', 151))).toBe('nome-max-150');
    });

    it('deve permitir não ter sigla', function () {
      expect(validador.sigla('')).toBeUndefined();
      expect(validador.sigla()).toBeUndefined();
    });

    it('sigla deve ter no máximo 15 caracteres', function () {
      expect(validador.sigla('012345678901234')).toBeUndefined();
      expect(validador.sigla('0123456789012345')).toBe('sigla-max-15');
    });

    it('nomes populares não são obrigatórios', function () {
      expect(validador.nomesPopulares([]).length).toBe(0);
    });

    it('nomes populares devem ter no máximo 150 caracteres', function () {
      var es = validador.nomesPopulares([_.repeat('a', 151), 'a']);

      expect(es).toBeDefined();
      expect(es.length).toBe(1);

      expect(es[0].i).toBe(0);
      expect(es[0].err).toBe('nome-pop-max-150');
    });

    it('validação nomes populares devem vir indexados', function () {
      var es = validador.nomesPopulares([_.repeat('a', 151), 'a', _.repeat('b', 151), 'b', _.repeat('x', 151)]);

      expect(es).toBeDefined();
      expect(es.length).toBe(3);
      expect(es[0].i).toBe(0);
      expect(es[1].i).toBe(2);
      expect(es[2].i).toBe(4);
    });
  });

});