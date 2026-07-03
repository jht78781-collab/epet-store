const state = {
    owner: null,
    pets: [],
    stores: [],
    ownedPets: [],
    search: '',
    storeId: 0
};

const API_BASE = (window.VITE_API_URL || window.EPET_API_BASE_URL || '').replace(/\/$/, '');

const typeAssets = {
    dog: 'dog.svg',
    penguin: 'penguin.svg',
    bird: 'bird.svg',
    tiger: 'tiger.svg',
    lion: 'lion.svg'
};

const typeNames = {
    dog: '小狗',
    penguin: '企鹅',
    bird: '小鸟',
    tiger: '虎',
    lion: '狮子'
};

const elements = {
    loginPanel: document.querySelector('#loginPanel'),
    profilePanel: document.querySelector('#profilePanel'),
    loginForm: document.querySelector('#loginForm'),
    logoutButton: document.querySelector('#logoutButton'),
    ownerName: document.querySelector('#ownerName'),
    ownerId: document.querySelector('#ownerId'),
    ownerMoney: document.querySelector('#ownerMoney'),
    stockCount: document.querySelector('#stockCount'),
    storeCount: document.querySelector('#storeCount'),
    ownedCount: document.querySelector('#ownedCount'),
    storeBalance: document.querySelector('#storeBalance'),
    storeFilter: document.querySelector('#storeFilter'),
    searchInput: document.querySelector('#searchInput'),
    refreshButton: document.querySelector('#refreshButton'),
    inventoryCount: document.querySelector('#inventoryCount'),
    petGrid: document.querySelector('#petGrid'),
    ownedHint: document.querySelector('#ownedHint'),
    ownedList: document.querySelector('#ownedList'),
    toast: document.querySelector('#toast')
};

init();

function init() {
    const savedOwner = localStorage.getItem('epet.owner');
    if (savedOwner) {
        try {
            state.owner = JSON.parse(savedOwner);
        } catch (error) {
            localStorage.removeItem('epet.owner');
        }
    }

    elements.loginForm.addEventListener('submit', login);
    elements.logoutButton.addEventListener('click', logout);
    elements.refreshButton.addEventListener('click', () => loadData(true));
    elements.searchInput.addEventListener('input', (event) => {
        state.search = event.target.value.trim().toLowerCase();
        renderPets();
    });
    elements.storeFilter.addEventListener('change', (event) => {
        state.storeId = Number(event.target.value);
        loadPets();
    });
    elements.petGrid.addEventListener('click', buyPet);

    renderAccount();
    loadData(false);
}

async function login(event) {
    event.preventDefault();
    const formData = new FormData(elements.loginForm);
    try {
        const data = await api('/api/login', {
            method: 'POST',
            body: JSON.stringify({
                name: formData.get('name'),
                password: formData.get('password')
            })
        });
        state.owner = data.owner;
        localStorage.setItem('epet.owner', JSON.stringify(state.owner));
        await loadOwner();
        renderAccount();
        renderPets();
        toast('登录成功');
    } catch (error) {
        toast(error.message, true);
    }
}

function logout() {
    state.owner = null;
    state.ownedPets = [];
    localStorage.removeItem('epet.owner');
    renderAccount();
    renderPets();
    renderOwnedPets();
}

async function buyPet(event) {
    const button = event.target.closest('[data-buy-id]');
    if (!button || !state.owner) {
        return;
    }

    const petId = Number(button.dataset.buyId);
    button.disabled = true;
    try {
        const data = await api('/api/buy', {
            method: 'POST',
            body: JSON.stringify({
                ownerId: state.owner.id,
                petId
            })
        });
        state.owner = data.owner;
        state.ownedPets = data.pets;
        localStorage.setItem('epet.owner', JSON.stringify(state.owner));
        await loadPets();
        renderAccount();
        renderOwnedPets();
        toast(`${data.message}，花费 ${data.price} 元宝`);
    } catch (error) {
        toast(error.message, true);
        button.disabled = false;
    }
}

async function loadData(showMessage) {
    try {
        await Promise.all([loadStores(), loadPets()]);
        if (state.owner) {
            await loadOwner();
        }
        renderAccount();
        renderMetrics();
        renderOwnedPets();
        if (showMessage) {
            toast('已刷新');
        }
    } catch (error) {
        toast(error.message, true);
    }
}

async function loadPets() {
    const query = state.storeId ? `?storeId=${state.storeId}` : '';
    const data = await api(`/api/pets${query}`);
    state.pets = data.pets;
    renderPets();
    renderMetrics();
}

async function loadStores() {
    const data = await api('/api/stores');
    state.stores = data.stores;
    renderStoreFilter();
    renderMetrics();
}

async function loadOwner() {
    if (!state.owner) {
        return;
    }
    const data = await api(`/api/owner?id=${state.owner.id}`);
    state.owner = data.owner;
    state.ownedPets = data.pets;
    localStorage.setItem('epet.owner', JSON.stringify(state.owner));
}

async function api(path, options = {}) {
    const response = await fetch(`${API_BASE}${path}`, {
        headers: { 'Content-Type': 'application/json' },
        ...options
    });
    const data = await response.json().catch(() => ({}));
    if (!response.ok) {
        throw new Error(data.message || '请求失败');
    }
    return data;
}

function renderAccount() {
    const loggedIn = Boolean(state.owner);
    elements.loginPanel.classList.toggle('hidden', loggedIn);
    elements.profilePanel.classList.toggle('hidden', !loggedIn);

    if (state.owner) {
        elements.ownerName.textContent = state.owner.name;
        elements.ownerId.textContent = state.owner.id;
        elements.ownerMoney.textContent = state.owner.money;
    }
    renderMetrics();
    renderOwnedPets();
}

function renderStoreFilter() {
    const currentValue = String(state.storeId);
    elements.storeFilter.innerHTML = '<option value="0">全部门店</option>';
    for (const store of state.stores) {
        const option = document.createElement('option');
        option.value = store.id;
        option.textContent = store.name;
        elements.storeFilter.append(option);
    }
    elements.storeFilter.value = currentValue;
}

function renderMetrics() {
    elements.stockCount.textContent = state.pets.length;
    elements.storeCount.textContent = state.stores.length;
    elements.ownedCount.textContent = state.ownedPets.length;
    elements.storeBalance.textContent = state.stores.reduce((sum, store) => sum + store.balance, 0);
}

function renderPets() {
    const search = state.search;
    const filteredPets = state.pets.filter((pet) => {
        const text = `${pet.name} ${pet.type} ${typeNames[pet.type] || ''}`.toLowerCase();
        return !search || text.includes(search);
    });

    elements.inventoryCount.textContent = `${filteredPets.length} 只`;
    elements.petGrid.innerHTML = '';

    if (filteredPets.length === 0) {
        elements.petGrid.innerHTML = '<div class="empty-state">暂无匹配的库存宠物</div>';
        return;
    }

    for (const pet of filteredPets) {
        elements.petGrid.append(createPetCard(pet));
    }
}

function createPetCard(pet) {
    const canBuy = state.owner && state.owner.money >= pet.price;
    const card = document.createElement('article');
    card.className = 'pet-card';
    card.innerHTML = `
        <div class="pet-card-media">
            <img src="${assetFor(pet.type)}" alt="${labelFor(pet.type)}">
        </div>
        <div class="pet-info">
            <div class="pet-title">
                <h4>${escapeHtml(pet.name)}</h4>
                <span class="type-badge">${labelFor(pet.type)}</span>
            </div>
            <dl class="pet-meta">
                <div>
                    <dt>编号</dt>
                    <dd>${pet.id}</dd>
                </div>
                <div>
                    <dt>门店</dt>
                    <dd>${pet.storeId}</dd>
                </div>
                <div>
                    <dt>生日</dt>
                    <dd>${pet.birthday || '-'}</dd>
                </div>
                <div>
                    <dt>价格</dt>
                    <dd>${pet.price} 元宝</dd>
                </div>
            </dl>
            <div class="pet-bars" aria-label="宠物状态">
                ${bar('健康', pet.health)}
                ${bar('亲密', pet.love)}
            </div>
            <div class="pet-actions">
                <span class="price">${pet.price}</span>
                <button type="button" class="buy-button" data-buy-id="${pet.id}" ${canBuy ? '' : 'disabled'}>
                    <span aria-hidden="true">＋</span>
                    ${state.owner ? (canBuy ? '购买' : '元宝不足') : '请登录'}
                </button>
            </div>
        </div>
    `;
    return card;
}

function renderOwnedPets() {
    elements.ownedList.innerHTML = '';
    if (!state.owner) {
        elements.ownedHint.textContent = '未登录';
        elements.ownedList.innerHTML = '<div class="empty-state">登录后显示已购买宠物</div>';
        return;
    }

    elements.ownedHint.textContent = `${state.ownedPets.length} 只`;
    if (state.ownedPets.length === 0) {
        elements.ownedList.innerHTML = '<div class="empty-state">暂无已购买宠物</div>';
        return;
    }

    for (const pet of state.ownedPets) {
        const row = document.createElement('div');
        row.className = 'owned-row';
        row.innerHTML = `
            <img src="${assetFor(pet.type)}" alt="">
            <div>
                <strong>${escapeHtml(pet.name)}</strong>
                <span>编号 ${pet.id} · 生日 ${pet.birthday || '-'}</span>
            </div>
            <span class="type-badge">${labelFor(pet.type)}</span>
        `;
        elements.ownedList.append(row);
    }
}

function bar(label, value) {
    const width = Math.max(0, Math.min(100, Number(value) || 0));
    return `
        <div class="bar-row">
            <span class="bar-label">${label}</span>
            <span class="bar-track"><span class="bar-fill" style="width: ${width}%"></span></span>
            <span class="bar-label">${width}</span>
        </div>
    `;
}

function assetFor(type) {
    return `assets/${typeAssets[type] || 'pet.svg'}`;
}

function labelFor(type) {
    return typeNames[type] || type || '宠物';
}

function toast(message, isError = false) {
    elements.toast.textContent = message;
    elements.toast.classList.toggle('error', isError);
    elements.toast.classList.add('show');
    clearTimeout(toast.timer);
    toast.timer = setTimeout(() => {
        elements.toast.classList.remove('show');
    }, 2400);
}

function escapeHtml(value) {
    return String(value)
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}
