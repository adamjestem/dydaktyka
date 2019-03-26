(function($) {

    $.fn.czcionka = function(font) {
        this.each(function() {
            $(this).css('font-family',font);
        });
        return this;
    };

})(jQuery);