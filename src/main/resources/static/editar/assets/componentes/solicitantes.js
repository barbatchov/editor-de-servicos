var Solicitantes = {
  controller: function (args) {
    this.solicitantes = args.solicitantes;

    this.adicionar = function() {
      this.solicitantes.push(new models.Solicitante());
    };

    this.remover = function(i) {
      this.solicitantes.splice(i, 1);
    };

  },
  view: function (ctrl) {
    return m('#solicitantes', [
      m('h2', 'Quem pode utilizar este serviço?'),

      ctrl.solicitantes.map(function(s, i) {
        return m('fieldset', {
          key: s.id
        }, [
          m('h3', 'Solicitante'),

          m("input.inline.inline-xg[type='text']", {
            value: s.descricao(),
            onchange: m.withAttr('value', s.descricao)
          }),

          m("button.inline.remove-peq", {
            onclick: ctrl.remover.bind(ctrl, i)
          }, [
            m("span.fa.fa-times")
          ]),

          m('h3', 'Requisitos'),
          m.component(EditorMarkdown, {
            rows: 5,
            value: s.requisitos(),
            onchange: m.withAttr('value', s.requisitos)
          })
        ]);
      }),

      m("button.adicionar-solicitante", {
        onclick: ctrl.adicionar.bind(ctrl)
      }, [
        m("i.fa.fa-plus"),
        " Adicionar solicitante "
      ])
    ]);
  }
};
