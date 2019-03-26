(function($) {

    $.fn.zmien_kolor = function(parametry) {
    
        if (parametry && typeof parametry == 'string') {
            this.each(function() {
                $(this).html(parametry);
            });
        } else if (parametry && typeof parametry == 'object') {
            var opcje = {
                tekst: 'Witaj na dzisiejszym laboratorium',
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

        } else {
            this.each(function() {
                $(this).html('BRAK PARAMETRÓW');
            });
        }
    
        
        return this;
    };

})(jQuery);