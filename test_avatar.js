const AZURIOM_URL = 'https://nexaria.netlib.re'
const fetch = require('node-fetch')
async function check() {
  const username = 'iPierre74'
  const res2 = await fetch(`${AZURIOM_URL}/api/skin-api/avatars/combo/${username}?size=150`)
  console.log('combo headers:', res2.headers.raw())
  
  // Actually download the first 100 bytes to see if it's a real image or redirect
  const buffer = await res2.buffer()
  console.log('size:', buffer.length)
}
check()
