const STORAGE_KEYS = {
  theme: "fliprate_theme",
  favorites: "fliprate_favorites",
  wishlist: "fliprate_wishlist",
};

const MAX_LIST_LENGTH = 10;

function getTheme() {
  return localStorage.getItem(STORAGE_KEYS.theme);
}

function setTheme(theme) {
  localStorage.setItem(STORAGE_KEYS.theme, theme);
}

function getFavorites() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEYS.favorites)) || [];
  } catch {
    return [];
  }
}

function saveFavorites(favorites) {
  localStorage.setItem(STORAGE_KEYS.favorites, JSON.stringify(favorites));
}

function getWishlist() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEYS.wishlist)) || [];
  } catch {
    return [];
  }
}

function saveWishlist(wishlist) {
  localStorage.setItem(STORAGE_KEYS.wishlist, JSON.stringify(wishlist));
}
