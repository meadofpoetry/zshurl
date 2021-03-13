/*
function utf8_to_b64( str ) {
    return window.btoa(unescape(encodeURIComponent( str )));
}

function b64_to_utf8( str ) {
    return decodeURIComponent(escape(window.atob( str )));
}
*/
const Form = () => {
    
    const [name, setName] = React.useState('');

    async function sendURL() {
        const response = await fetch("/api/shortify", {
            method: 'POST', // *GET, POST, PUT, DELETE, etc.
            mode: 'cors', // no-cors, *cors, same-origin
            cache: 'no-cache', // *default, no-cache, reload, force-cache, only-if-cached
            credentials: 'same-origin', // include, *same-origin, omit
            headers: {
                'Content-Type': 'application/json'
            },
            //redirect: 'follow', // manual, *follow, error
            //referrerPolicy: 'no-referrer', // no-referrer, *no-referrer-when-downgrade, origin, origin-when-cross-origin, same-origin, strict-origin, strict-origin-when-cross-origin, unsafe-url
            body: JSON.stringify({ url: name }) // body data type must match "Content-Type" header
        });
        const res = await response.json();
        return res.url;
    }
    
    async function handleSubmit(e) {    
        e.preventDefault();
        if (name) {
            sendURL()
                .then(newName => setName(newName))
                .catch(console.error('Failed to shortify URL'))
        } else {
            console.log(`Nothing to send`);
        }
        //setName('updated');
        console.log(`Form submitted, ${name}`);
    }

    return(
        <form onSubmit = {handleSubmit}>
            <input onChange = {(e) => setName(e.target.value)} value = {name}></input>
            <button type = 'submit'>Click to submit</button>
        </form>
    );
}

ReactDOM.render(
  <Form />,
  document.getElementById('root')
);
