/**
 * Vellfire — Feedback page interactions
 * Star ratings and washroom toggle
 */

document.addEventListener('DOMContentLoaded', function () {

    // ----- Star rating widgets -----
    document.querySelectorAll('.star-rating').forEach(function (widget) {
        const fieldName = widget.dataset.field;
        const hiddenInput = document.getElementById(fieldName + 'Input');
        const stars = widget.querySelectorAll('.star');

        stars.forEach(function (star) {
            star.addEventListener('mouseenter', function () {
                const val = parseInt(star.dataset.value);
                stars.forEach(function (s) {
                    s.textContent = parseInt(s.dataset.value) <= val ? '★' : '☆';
                    s.style.color  = parseInt(s.dataset.value) <= val ? '#f59e0b' : '#d1d5db';
                });
            });

            star.addEventListener('mouseleave', function () {
                const current = hiddenInput ? parseInt(hiddenInput.value) || 0 : 0;
                stars.forEach(function (s) {
                    const sv = parseInt(s.dataset.value);
                    s.textContent = sv <= current ? '★' : '☆';
                    s.style.color  = sv <= current ? '#f59e0b' : '#d1d5db';
                });
            });

            star.addEventListener('click', function () {
                const val = parseInt(star.dataset.value);
                if (hiddenInput) hiddenInput.value = val;
                stars.forEach(function (s) {
                    const sv = parseInt(s.dataset.value);
                    s.textContent = sv <= val ? '★' : '☆';
                    s.style.color  = sv <= val ? '#f59e0b' : '#d1d5db';
                    s.classList.toggle('active', sv <= val);
                });
            });
        });
    });
});

// ----- Washroom toggle -----
function selectWashroom(value) {
    const yesBtn = document.getElementById('toggleYes');
    const noBtn  = document.getElementById('toggleNo');
    const input  = document.getElementById('washroomAvailableInput');

    if (value === 'YES') {
        yesBtn.classList.add('selected');
        noBtn.classList.remove('selected');
    } else {
        noBtn.classList.add('selected');
        yesBtn.classList.remove('selected');
    }

    if (input) input.value = value;
}
