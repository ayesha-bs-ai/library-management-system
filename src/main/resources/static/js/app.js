document.addEventListener('DOMContentLoaded', () => {
  const menuButton = document.querySelector('[data-menu-button]');
  const mobileMenu = document.querySelector('[data-mobile-menu]');
  if (menuButton && mobileMenu) {
    menuButton.addEventListener('click', () => {
      const open = mobileMenu.classList.toggle('hidden') === false;
      menuButton.setAttribute('aria-expanded', String(open));
    });
  }

  document.querySelectorAll('[data-confirm]').forEach((form) => {
    form.addEventListener('submit', (event) => {
      const message = form.dataset.confirm || 'Are you sure?';
      if (!window.confirm(message)) event.preventDefault();
    });
  });

  document.querySelectorAll('[data-auto-focus]').forEach((field) => field.focus());

  window.setTimeout(() => {
    document.querySelectorAll('[data-flash]').forEach((element) => {
      element.classList.add('opacity-0', '-translate-y-2');
      window.setTimeout(() => element.remove(), 250);
    });
  }, 5000);
});
