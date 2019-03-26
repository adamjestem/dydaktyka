
(function($) {

    $.fn.pierwszy = function() {
        this.each(function() {
            $(this).html('Nowy Rok');
        });
        return this;
    };

    $.fn.ostatni = function() {
        this.each(function() {
            $(this).html('Sylwester');
        });
        return this;
    };

})(jQuery);