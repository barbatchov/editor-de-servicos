'use strict';

module.exports = {

  controller: function (args) {
    this.servico = args.servico;

    this.adicionar = function () {
      this.servico().palavrasChave().push('');
    };

    this.remover = function (i) {
      this.servico().palavrasChave().splice(i, 1);
    };
  },

  view: function (ctrl) {
    return m('fieldset#palavras-chave', [

      m('h3', [
        'Palavras-chave',
        m.component(require('tooltips').palavrasChave)
      ]),

      ctrl.servico().palavrasChave().map(function (palavras, i) {
        return [
          m('input.inline[type=text]', {
            value: palavras,
            onchange: function (e) {
              ctrl.servico().palavrasChave()[i] = e.target.value;
            }
          }),
          m('button.remove', {
            onclick: ctrl.remover.bind(ctrl, i)
          }, [
            m('span.fa.fa-trash-o')
          ])
        ];
      }),

      m('button.adicionar.adicionar-palavra-chave', {
        onclick: ctrl.adicionar.bind(ctrl)
      }, [
        m('i.fa.fa-plus'),
        ' Adicionar palavra-chave '
      ])
    ]);
  }
};
