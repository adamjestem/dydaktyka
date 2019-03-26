(function($) {
    $.fn.tabela = function(dane) {
    
        var tmp = this.first();

        $.ajax({
            url: dane,
            timeout: 2500,
            cache: false,
            success: function(html){

                var wiersze = html.split("\n");

                var tabelka = $('<table/>').appendTo(tmp);

                var tr = $('<tr/>').appendTo(tabelka);
                $('<th/>').text('lp.').appendTo(tr);
                var kolumny = wiersze[0].split('|');
                $.each(kolumny, function(index){
                    $('<th/>').html(kolumny[index]).appendTo(tr);
                });

                wiersze.shift();

                $.each(wiersze, function(index) {
                    var tr = $('<tr/>').appendTo(tabelka);
                    $('<td/>').text((index + 1) + '.').appendTo(tr);
                    var kolumny = wiersze[index].split('|');
                    $.each(kolumny, function(index){
                        $('<td/>').html(kolumny[index]).appendTo(tr);
                    });
                });
            }
        });

        return this;
    };
})(jQuery);