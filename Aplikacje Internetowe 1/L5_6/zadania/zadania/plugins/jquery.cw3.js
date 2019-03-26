(function($) {
    $.fn.zmien_kolor = function(parametry) {

        var opcje = {
            tekst: 'Witaj na dzisiejszym laboratorium !',
            kolor: 'blue',
            tlo:   'yellow'
        };

        if (parametry) {
            $.extend(opcje, parametry);
        }

        this.each(function() {
            $(this)
                .css('color', opcje.kolor)
                .css('background', opcje.tlo)
                .html(opcje.tekst);
        });

        return this;
    };
})(jQuery);